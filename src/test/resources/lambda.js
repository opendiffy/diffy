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
        body: {
                "message": "Success",
                "response": {
                  "id": 98758734857,
                  "app": 12,
                  "resource": {
                    "id": 150,
                    "app": null,
                    "label": "Test label",
                    "hfu": {
                      "id": 898,
                      "app": null,
                      "createdDate": null,
                      "lastModifiedDate": null,
                      "name": "test_name",
                      "uuid": null
                    },
                    "custom": false,
                    "delhuf": false
                  },
                  "title": "Test title from candidate server",
                  "abcd": "text",
                  "related": [
                    "ioi"
                  ],
                  "data": null,
                  "flabel": "Test",
                  "tcon": null,
                  "type": "UYU",
                  "gb": [],
                  "meas": null,
                  "audy": null,
                  "stpe": null,
                  "slim": 0,
                  "zoomInData": null,
                  "improvement": null,
                  "isdterange": true,
                  "meta": null,
                  "intcof": null,
                  "metqu": null,
                  "fif": {
                    "id": null,
                    "requestResource": null,
                    "resource": null,
                    "updatable": false,
                    "empty": true
                  },
                  "modified": false,
                  "description": null,
                  "initdat": null,
                  "applyModifiedData": false,
                  "sortConfiguration": null,
                  "pasfor": null,
                  "originalTitle": "Test original title",
                  "uuid": "345345346",
                  "cofn": null,
                  "rol": null,
                  "accessible": true,
                  "hasDel": false,
                  "witype": null,
                  "empwi": false,
                  "syd": null,
                  "asgr": [],
                  "ismod": false,
                  "ughuygegg": false,
                  "deleted": false,
                  "newef": false,
                  "efor": null
                },
                "path": null
              }
    }

    response.body = body;
    return response;
}