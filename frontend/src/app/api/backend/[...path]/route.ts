import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const cookieName = "referral_session";
type Context = { params: Promise<{ path: string[] }> };

async function proxy(request: NextRequest, context: Context) {
  const token = (await cookies()).get(cookieName)?.value;
  if (!token) return NextResponse.json({ message: "Your session has expired. Please sign in again." }, { status: 401 });
  const { path } = await context.params;
  const baseUrl = (process.env.CRM_API_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");
  const upstreamUrl = `${baseUrl}/${path.map(encodeURIComponent).join("/")}${request.nextUrl.search}`;
  const headers = new Headers({ Authorization: `Bearer ${token}` });
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("Content-Type", contentType);
  try {
    const upstream = await fetch(upstreamUrl, {
      method: request.method,
      headers,
      body: ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer(),
      cache: "no-store",
    });
    const responseBody = upstream.status === 204 ? null : await upstream.arrayBuffer();
    const responseHeaders = new Headers({ "Content-Type": upstream.headers.get("content-type") ?? "application/json" });
    const disposition = upstream.headers.get("content-disposition");
    if (disposition) responseHeaders.set("Content-Disposition", disposition);
    return new NextResponse(responseBody, { status: upstream.status, headers: responseHeaders });
  } catch {
    return NextResponse.json({ message: "CRM API is unavailable" }, { status: 503 });
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
