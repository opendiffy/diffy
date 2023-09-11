(request) => {
    const {
        uri,
        method,
        path,
        params,
        headers,
        body
    } = request;

    delete headers['Content-Length']
    return {
        status: '200 OK',
        headers,
        body
    }
}