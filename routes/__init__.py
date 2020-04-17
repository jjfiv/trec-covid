import flask
from flask import Flask, g, Response, session
import json, os
from typing import Optional
from irene import IreneService, IreneIndex

# Turn logging down to errors:
# https://stackoverflow.com/a/18379764/1057048
import logging

log = logging.getLogger("werkzeug")
log.setLevel(logging.ERROR)

app = Flask(__name__)
app.url_map.strict_slashes = False
app.config.from_object(__name__)


@app.route("/robots.txt")
@app.route("/ROBOTS.TXT")
def robots_txt():
    return Response(
        """User agent: *
disallow: /
""",
        mimetype="text/plain",
    )


@app.teardown_appcontext
def close_db(_err):
    if hasattr(g, "db"):
        g.db.commit()
        g.db.close()
        del g.db


def get_db():
    if not hasattr(g, "db"):
        g.db = connect_db()
    return g.db


def get_user() -> Optional[str]:
    if "user" not in session:
        return None
    return session["user"]


from .search_results import *
