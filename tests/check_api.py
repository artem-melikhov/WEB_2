import argparse
import json
import time
from urllib.error import HTTPError, URLError
from urllib.request import urlopen


CASES = [
    ("треугольник: внутри", "x=-1&y=0.5&r=2", 200, True),
    ("прямоугольник: внутри", "x=-1&y=-0.5&r=2", 200, True),
    ("четверть круга: внутри", "x=0&y=0.5&r=2", 200, True),
    ("вне области", "x=1&y=1&r=2", 200, False),
    ("граница области", "x=0&y=1&r=2", 200, True),
    ("Y чуть за границей", "x=0&y=1.000000000000000000000000000001&r=2", 200, False),
    ("лишний параметр", "x=1&y=1&r=1&extra=143", 200, False),
    ("повтор X", "x=1&x=2&y=1&r=2", 400, None),
    ("повтор Y", "x=1&y=1&y=2&r=2", 400, None),
    ("повтор R", "x=1&y=1&r=2&r=3", 400, None),
    ("нет R", "x=1&y=1", 400, None),
    ("дробный X", "x=1.999999999999&y=0&r=2", 400, None),
    ("Y вне диапазона", "x=0&y=5&r=2", 400, None),
    ("неверный R", "x=0&y=0&r=4", 400, None),
]


def request(url):
    started = time.perf_counter()
    try:
        with urlopen(url, timeout=5) as response:
            status = response.status
            body = response.read()
    except HTTPError as error:
        status = error.code
        body = error.read()
    elapsed_ms = (time.perf_counter() - started) * 1000
    return status, json.loads(body), elapsed_ms


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:18080",
                        help="адрес Apache (по умолчанию локальный запуск)")
    parser.add_argument("--repeat", type=int, default=10,
                        help="число запросов для каждого случая (по умолчанию 10)")
    args = parser.parse_args()
    if args.repeat < 1:
        parser.error("--repeat должен быть больше нуля")

    endpoint = args.url.rstrip("/") + "/api/check?"
    failed = 0
    print(f"{'Итог':<6} {'Успешно':<8} {'Случай':<27} "
          f"{'HTTP мин, мс':>13} {'HTTP ср, мс':>13} "
          f"{'HTTP макс, мс':>14} {'Java ср, мс':>13}")

    for name, query, expected_status, expected_hit in CASES:
        times = []
        server_times = []
        problems = []
        for _ in range(args.repeat):
            try:
                status, data, elapsed_ms = request(endpoint + query)
            except (URLError, TimeoutError, ValueError) as error:
                problems.append(str(error))
                continue

            times.append(elapsed_ms)
            if status == 200 and isinstance(data, dict) and isinstance(data.get("durationMs"), (int, float)):
                server_times.append(data["durationMs"])
            if not isinstance(data, dict):
                problems.append("ответ не является JSON-объектом")
                continue
            if status != expected_status:
                problems.append(f"ожидался HTTP {expected_status}, получен {status}")
            elif expected_hit is not None and data.get("hit") is not expected_hit:
                problems.append(f"ожидалось hit={expected_hit}, получено {data.get('hit')}")
            elif expected_hit is None and not isinstance(data.get("error"), str):
                problems.append("в ответе 400 нет сообщения об ошибке")

        if problems:
            failed += 1
        verdict = "FAIL" if problems else "OK"
        http_min = f"{min(times):.2f}" if times else "—"
        http_avg = f"{sum(times) / len(times):.2f}" if times else "—"
        http_max = f"{max(times):.2f}" if times else "—"
        java_avg = f"{sum(server_times) / len(server_times):.3f}" if server_times else "—"
        passed = f"{args.repeat - len(problems)}/{args.repeat}"
        print(f"{verdict:<6} {passed:<8} {name:<27} "
              f"{http_min:>13} {http_avg:>13} {http_max:>14} {java_avg:>13}")
        if problems:
            print(f"      {problems[0]}")

    print(f"\nПроверено случаев: {len(CASES)}, ошибок: {failed}")


if __name__ == "__main__":
    main()
