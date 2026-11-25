#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
旧静态知识库（wms-erp-kb/kb-data.js）→ it-kb 平台迁移脚本
================================================================
用法（在 it-kb 目录下，环境参数见部署手册）：
  PY kb-migrate.py --kb-data ../wms-erp-kb/kb-data.js --api http://localhost:8080 --admin admin --password admin@123

规则（与架构文档第九节一致）：
1. 25 条 qa 卡 → POST /api/docs（doc_type=qa，content_json=结构化字段）
2. system/category/owner/keywords 口径原样保留（system=工单归属系统，不是处理落点）
3. 示例卡（sourceNo == "示例"）不迁
4. 卡片里 images[].src 若是 /api/files/... 直接保留；旧静态页 base64 图需先走文件导入（二期）
5. 导入到「相关问题和业务文档」空间（space_code=QA，脚本自动查询 spaceId）
"""
import argparse
import json
import re
import sys
import time

try:
    import urllib.request as rq
except ImportError:
    sys.exit("需要 Python3")


def http(method, url, body=None, token=None, timeout=30):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = rq.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with rq.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def load_kb_data(path):
    text = open(path, encoding="utf-8").read()
    m = re.search(r"window\.KB_DATA\s*=\s*(\[.*?\])\s*;?\s*$", text, re.S)
    if not m:
        m = re.search(r"window\.KB_DATA\s*=\s*(\[.*?\])", text, re.S)
    if not m:
        sys.exit("kb-data.js 里找不到 window.KB_DATA 数组")
    return json.loads(m.group(1))


def qa_content(card):
    """旧卡片字段 → 平台 content_json（口径原样保留）"""
    return json.dumps({
        "errorText": card.get("errorText") or "",
        "cause": card.get("cause") or "",
        "sql": card.get("sql") or "",
        "steps": card.get("steps") or [],
        "tips": card.get("tips") or [],
        "images": (card.get("images") or []),
        "sourceNo": card.get("sourceNo") or "",
        "status": card.get("status") or "已解决",
    }, ensure_ascii=False)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kb-data", required=True, help="旧 kb-data.js 路径")
    ap.add_argument("--api", default="http://localhost:8080", help="平台地址（到端口）")
    ap.add_argument("--admin", default="admin")
    ap.add_argument("--password", default="admin@123")
    ap.add_argument("--dry-run", action="store_true", help="只打印将导入的卡片")
    args = ap.parse_args()

    cards = load_kb_data(args.kb_data)
    real = [c for c in cards if c.get("sourceNo") != "示例"]
    print("kb-data 共 %d 条，示例卡 %d 条（不迁），实际导入 %d 条"
          % (len(cards), len(cards) - len(real), len(real)))
    if args.dry_run:
        for c in real:
            print(" -", c.get("id"), c.get("question", "")[:40])
        return

    login = http("POST", args.api + "/api/auth/login",
                 {"username": args.admin, "password": args.password})
    token = login["data"]["token"]
    spaces = http("GET", args.api + "/api/spaces")["data"]
    qa_space = [s for s in spaces if s.get("spaceCode") == "QA"]
    if not qa_space:
        sys.exit("找不到 space_code=QA 的空间（先执行 sql/init.sql 或在管理端创建）")
    space_id = qa_space[0]["id"]

    ok = fail = 0
    for c in real:
        body = {
            "spaceId": space_id,
            "docType": "qa",
            "title": c.get("question") or ("无标题-" + str(c.get("id"))),
            "system": c.get("system") or "",
            "category": c.get("category") or "",
            "owner": c.get("owner") or "",
            "summary": c.get("summary") or "",
            "contentJson": qa_content(c),
            "contentMd": c.get("errorText") or "",
            "tags": c.get("keywords") or "",
        }
        try:
            http("POST", args.api + "/api/docs", body, token)
            ok += 1
            print("ok  ", c.get("id"), body["title"][:40])
        except Exception as e:
            fail += 1
            print("FAIL", c.get("id"), str(e)[:120])
        time.sleep(0.05)

    print("完成：成功 %d / 失败 %d" % (ok, fail))
    print("提示：导入后旧静态页（workbuddy.link）保留只读过渡，公告指向新平台后再下线。")


if __name__ == "__main__":
    main()
