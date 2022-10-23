import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

export interface Info {
    name: string,
    primary: Target,
    secondary: Target,
    candidate: Target,
    relativeThreshold: number,
    last_reset: number,
    absoluteThreshold: number,
    protocol: string,
}
export interface Target {
    target: String
}
export const apiInfoSlice = createApi({
    reducerPath: 'info',
    baseQuery: fetchBaseQuery({
        baseUrl: '/api/1'
    }),
    endpoints(builder) {
        return {
            fetchInfo: builder.query<Info, void>({
                query(){
                    return '/info';
                }
            })
        }
    },
});

export const {useFetchInfoQuery} = apiInfoSlice;
export function fetchinfo(){
    const target = 'Unknown';
    return useFetchInfoQuery().data || {
        name: 'Unknown',
        primary: {target},
        secondary: {target},
        candidate: {target},
        relativeThreshold: 20,
        last_reset: 0,
        absoluteThreshold: 0.03,
        protocol: "http",
    }
}