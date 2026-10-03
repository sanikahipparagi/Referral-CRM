import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export async function GET() {
  const token = (await cookies()).get("referral_session")?.value;
  if (!token) return NextResponse.json({ message: "Not signed in" }, { status: 401 });
  try {
    const baseUrl = (process.env.CRM_API_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");
    const upstream = await fetch(`${baseUrl}/auth/me`, { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" });
    if (!upstream.ok) {
      const response = NextResponse.json({ message: "Session expired" }, { status: 401 });
      response.cookies.delete("referral_session");
      return response;
    }
    return NextResponse.json({ user: await upstream.json() });
  } catch {
    return NextResponse.json({ message: "CRM API is unavailable" }, { status: 503 });
  }
}
