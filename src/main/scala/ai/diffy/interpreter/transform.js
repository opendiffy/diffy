(request) => {
console.log(request);
let result = request;
    result = {
    ...result,
        headers : {
            ...result.headers,
            "Content-type": "application/json"
        }
    };
    return result;
}