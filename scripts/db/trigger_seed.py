import asyncio
import httpx

BASE = "http://localhost:2026"

async def run():
    async with httpx.AsyncClient(base_url=BASE) as client:
        # 1. Login as SUPER_ADMIN
        print("Login as SUPER_ADMIN...")
        resp = await client.post("/api/auth/login", json={
            "correo": "marcelojp03@gmail.com",
            "contrasena": "SuperAdmin123!"
        })
        print(f"  Status: {resp.status_code}")
        data = resp.json()
        token = data.get("data", {}).get("token") or data.get("token")
        print(f"  Token: {str(token)[:50]}...")

        if not token:
            print(f"  Response: {resp.text[:300]}")
            return

        # 2. Call seed endpoint
        print("\nTriggering seed...")
        headers = {"Authorization": f"Bearer {token}"}
        resp = await client.post("/api/seed/synthetic", headers=headers)
        print(f"  Status: {resp.status_code}")
        print(f"  Response: {resp.text[:500]}")

asyncio.run(run())
