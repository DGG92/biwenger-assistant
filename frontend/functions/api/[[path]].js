const BACKEND_ORIGIN = 'https://raspdiego.tailcdc3f7.ts.net';

export async function onRequest(context) {
    const request = context.request;
    const incomingUrl = new URL(request.url);

    const backendUrl = new URL(
        incomingUrl.pathname + incomingUrl.search,
        BACKEND_ORIGIN
    );

    const headers = new Headers(request.headers);

    // Headers asociados al origen/navegador que no deben condicionar
    // la comunicación servidor -> servidor con Spring.
    headers.delete('host');
    headers.delete('origin');
    headers.delete('referer');

    const init = {
        method: request.method,
        headers,
        redirect: 'manual',
    };

    if (request.method !== 'GET' && request.method !== 'HEAD') {
        init.body = request.body;
    }

    try {
        const backendResponse = await fetch(
            backendUrl.toString(),
            init
        );

        return new Response(
            backendResponse.body,
            {
                status: backendResponse.status,
                statusText: backendResponse.statusText,
                headers: backendResponse.headers,
            }
        );
    } catch (error) {
        console.error('Backend proxy error:', error);

        return new Response(
            JSON.stringify({
                error: 'Backend unavailable',
            }),
            {
                status: 502,
                headers: {
                    'Content-Type': 'application/json',
                },
            }
        );
    }
}