import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

type Transformation = {injectionPoint:string, transformationJs:string}

export const apiTransformationsSlice = createApi({
    reducerPath: 'transformations',
    baseQuery: fetchBaseQuery({
        baseUrl: '/api/1'
    }),
    endpoints(builder) {
        return {
            fetchOverride: builder.query<Transformation, string>({
                query(injectionPoint){
                    return `/transformations/${injectionPoint}`;
                }
            }),
            updateOverride: builder.mutation<void, Transformation>({
                query({injectionPoint, transformationJs}){
                    return {
                        url: `/transformations/${injectionPoint}`,
                        method: 'POST',
                        body: transformationJs
                    }
                },
            })
        }
    },
});

export const {useFetchOverrideQuery, useUpdateOverrideMutation} = apiTransformationsSlice;