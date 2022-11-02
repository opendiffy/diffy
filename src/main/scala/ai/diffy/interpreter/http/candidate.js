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

    if(uri.startsWith('/api/v2/')){
        response.body = body
    } else {
        try {
            const json = JSON.parse(body)
            const result = {}
            Object.entries(json).foreach(([key, value]) => {
                result[key.toLowerCase()] = value
            })
            response.body = result
        } catch(e) {
            response.body = body
        }
    }
    return response;
}