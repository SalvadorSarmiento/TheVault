import http.client
import json
import os
import socket
import socketserver
import threading
import time
from contextlib import closing
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit

import uvicorn

os.environ["THEVAULT_AUTH_MODE"] = "development"
os.environ["THEVAULT_DATABASE_PATH"] = ":memory:"

from thevault.adapters.outbound.auth import DevelopmentTokenVerifier
from thevault.adapters.outbound.repositories import InMemoryCredentialRepository
from thevault.application.credential_service import CredentialService
from thevault.config import Settings
from thevault.main import create_app


# Solo datos ficticios en memoria. No usa la API publicada ni su base de datos.
app = create_app(
    settings=Settings(auth_mode="development"),
    repository=InMemoryCredentialRepository(),
    token_verifier=DevelopmentTokenVerifier(),
)
lock = threading.RLock()
estado = {"modo": "normal", "antes": False, "despues": False, "posts": 0}
puerta = threading.Event()
puerta.set()
conexiones = set()
proxy = None
fin = threading.Event()


class Puente(socketserver.StreamRequestHandler):
    def handle(self):
        with lock:
            conexiones.add(self.request)
        try:
            self.request.settimeout(20)
            linea = self.rfile.readline().decode("iso-8859-1").strip()
            if not linea:
                return
            metodo, ruta, _ = linea.split(" ", 2)
            headers = {}
            while True:
                linea = self.rfile.readline().decode("iso-8859-1").strip()
                if not linea:
                    break
                key, value = linea.split(":", 1)
                headers[key.strip()] = value.strip()
            cuerpo = self.rfile.read(int(headers.get("Content-Length", "0")))
            es_guardado = metodo == "POST" and ruta == "/api/v1/credentials"
            with lock:
                modo = estado["modo"]
            if es_guardado and modo == "antes":
                with lock:
                    estado["antes"] = True
                puerta.wait(30)
                return
            with closing(http.client.HTTPConnection("127.0.0.1", 18081, timeout=10)) as conexion:
                headers["Host"] = "127.0.0.1:18081"
                headers["Connection"] = "close"
                conexion.request(metodo, ruta, body=cuerpo, headers=headers)
                respuesta = conexion.getresponse()
                contenido = respuesta.read()
                if es_guardado:
                    with lock:
                        estado["posts"] += 1
                        estado["despues"] = respuesta.status == 201
                if es_guardado and modo == "despues":
                    puerta.wait(30)
                    return
                cabecera = (
                    f"HTTP/1.1 {respuesta.status} {respuesta.reason}\r\n"
                    "Content-Type: application/json\r\n"
                    f"Content-Length: {len(contenido)}\r\nConnection: close\r\n\r\n"
                )
                self.wfile.write(cabecera.encode("iso-8859-1") + contenido)
                self.wfile.flush()
        except (OSError, ValueError, http.client.HTTPException):
            pass
        finally:
            with lock:
                conexiones.discard(self.request)


class ServidorPuente(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


def desconectar():
    global proxy
    with lock:
        anterior, proxy = proxy, None
        activas = list(conexiones)
    if anterior:
        anterior.shutdown()
        anterior.server_close()
    for conexion in activas:
        try:
            conexion.shutdown(socket.SHUT_RDWR)
            conexion.close()
        except OSError:
            pass
    puerta.set()


def conectar():
    global proxy
    with lock:
        estado["modo"] = "normal"
        puerta.set()
        if proxy is None:
            proxy = ServidorPuente(("127.0.0.1", 18082), Puente)
            threading.Thread(target=proxy.serve_forever, kwargs={"poll_interval": 0.05}, daemon=True).start()


class Control(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_GET(self):
        url = urlsplit(self.path)
        if url.path == "/reiniciar":
            desconectar()
            app.state.credential_service = CredentialService(InMemoryCredentialRepository())
            with lock:
                estado.update(modo="normal", antes=False, despues=False, posts=0)
            conectar()
        elif url.path == "/desconectar":
            desconectar()
        elif url.path == "/conectar":
            conectar()
        elif url.path == "/modo":
            modo = parse_qs(url.query).get("valor", ["normal"])[0]
            with lock:
                estado.update(modo=modo, antes=False, despues=False)
                puerta.clear()
        elif url.path == "/terminar":
            fin.set()
        with lock:
            resultado = dict(estado)
            resultado["cuentas"] = len(app.state.credential_service.list_credentials("prueba-cortes"))
            resultado["conectado"] = proxy is not None
        cuerpo = json.dumps(resultado).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)


api = uvicorn.Server(uvicorn.Config(app, host="127.0.0.1", port=18081, log_level="error", access_log=False))
threading.Thread(target=api.run, daemon=True).start()
limite = time.monotonic() + 15
while not api.started:
    if time.monotonic() > limite:
        raise RuntimeError("La API local no arrancó")
    time.sleep(0.05)
conectar()
control = ThreadingHTTPServer(("127.0.0.1", 18083), Control)
threading.Thread(target=control.serve_forever, daemon=True).start()
print("Prueba lista: API local y conexión controlada, con datos ficticios.", flush=True)
fin.wait()
desconectar()
control.shutdown()
control.server_close()
api.should_exit = True
