(request) => {
    const {
        uri,
        method,
        path,
        params,
        headers,
        body
    } = request;

    const response = {
        status: '200 OK',
        headers: {},
        body: ''
    }

    response.body = request.body.toLowerCase();
    return response;
}