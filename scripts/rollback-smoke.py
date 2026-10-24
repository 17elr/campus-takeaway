"""Read-only legacy-JAR check against the explicitly named isolated rehearsal database."""
from pathlib import Path
from urllib.request import build_opener, HTTPCookieProcessor, Request
from urllib.parse import urlencode, urlparse
from http.cookiejar import CookieJar
import hashlib
import json
import os
import re
import subprocess

TABLES = ("users", "shops", "dishes", "addresses", "orders", "order_items", "order_events", "cart", "cart_item")
DATABASE = "campus_takeaway_rollback_test"
BASE = os.environ.get("LEGACY_URL", "http://127.0.0.1:18082")
assert urlparse(BASE).hostname == "127.0.0.1" and urlparse(BASE).scheme == "http", "Use the isolated local legacy server"


def snapshot():
    result = {}
    environment = os.environ.copy()
    environment["MYSQL_PWD"] = os.environ.get("REHEARSAL_DB_PASSWORD", "")
    for table in TABLES:
        data = subprocess.check_output([
            "mysql", "--host=127.0.0.1", "--port=13317", "--user=root", "--batch", "--raw",
            f"--database={DATABASE}", "--execute", f"SELECT * FROM {table} ORDER BY 1,2",
        ], env=environment)
        result[table] = {"rows": max(0, len(data.splitlines()) - 1), "sha256": hashlib.sha256(data).hexdigest()}
    return result


before = snapshot()
assert before["orders"]["rows"] > 0 and before["cart_item"]["rows"] > 0, "Prepare the demo-data copy first"
client = build_opener(HTTPCookieProcessor(CookieJar()))
login = client.open(BASE + "/login", timeout=15).read().decode("utf-8")
csrf = re.search(r'name="_csrf"[^>]*value="([^"]+)"', login)
assert csrf, "Legacy login must still require CSRF"
response = client.open(Request(BASE + "/login", data=urlencode({"username": "student", "password": "student123", "_csrf": csrf.group(1)}).encode()), timeout=15)
assert response.status == 200 and "/student" in response.url, response.url
page = client.open(BASE + "/student?tab=orders", timeout=15).read().decode("utf-8")
assert "已完成" in page, "Legacy UI must display the upgraded completed order"
after = snapshot()
assert before == after, "Legacy read must preserve all orders, snapshots, events and database carts"
result = {"legacyHttpStatus": response.status, "completedOrderVisible": True, "tablesUnchanged": after,
          "limit": "Legacy UI cannot display V2 database carts; they remain preserved for a future upgrade."}
output = Path("docs/rollback-results.json")
output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"legacyLogin": "passed", "completedOrder": "visible", "unchangedTables": len(after)}, ensure_ascii=False))
