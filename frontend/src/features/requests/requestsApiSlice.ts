import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { Request } from "./Request";

export const apiRequestsSlice = createApi({
    reducerPath: 'requests',
    baseQuery: fetchBaseQuery({
        baseUrl: '/api/1'
    }),
    endpoints(builder) {
        return {
            fetchRequest: builder.query<Request, string>({
                query(requestId){
                    return `/requests/${requestId}`;
                }
            })
        }
    },
});

export const {useFetchRequestQuery} = apiRequestsSlice;