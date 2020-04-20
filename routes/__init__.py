import flask
from flask import Flask, g, Response, session, request
import json, os
from typing import Optional
from irene import IreneService, IreneIndex
import sqlite3

# Turn logging down to errors:
# https://stackoverflow.com/a/18379764/1057048
import logging

log = logging.getLogger("werkzeug")
log.setLevel(logging.ERROR)

app = Flask(__name__)
app.url_map.strict_slashes = False
app.config.from_object(__name__)
if os.path.exists(".secret.json"):
    with open(".secret.json", "r") as fp:
        data = json.load(fp)
        app.config.update({"SECRET_KEY": data["SECRET_KEY"]})
        app.config.update({"TOKEN": data["TOKEN"]})
else:
    app.config.update({"SECRET_KEY": "this is a secret key"})
    app.config.update({"TOKEN": "debug"})


@app.route("/robots.txt")
@app.route("/ROBOTS.TXT")
def robots_txt():
    return Response(
        """User agent: *
disallow: /
""",
        mimetype="text/plain",
    )


def connect_db(path="labels.db") -> sqlite3.Connection:
    conn = sqlite3.connect(
        path, timeout=5, detect_types=sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES
    )
    conn.row_factory = sqlite3.Row
    return conn


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


@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        if request.form["user"]:
            session["user"] = request.form["user"]
    user = get_user()
    if user is not None:
        return flask.redirect(
            request.args.get(
                "destination", request.form.get("destination", url_for("home_page"))
            )
        )
    return flask.render_template("login.j2")


from .search_results import *
