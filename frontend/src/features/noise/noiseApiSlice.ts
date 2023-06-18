import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { DifferenceResults } from "../differences/DifferenceResults";
import { DifferencesQueryArgs } from "../differences/DifferencesQueryArgs";

import { EndpointMeta } from "../endpoints/EndpointMeta";
import { Endpoint } from "../fields/Endpoint";
import { EndpointFieldPrefix } from "../fields/EndpointFieldPrefix";
import { FieldsQueryArgs } from "../fields/FieldsQueryArgs";

export const apiNoiseSlice = createApi({
    reducerPath: 'noiseApi',
    tagTypes: ['Noise'],
    baseQuery: fetchBaseQuery({
        baseUrl: '/api/1',
    }),
    endpoints(builder) {
        return {
            fetchNoise: builder.query<string[], string>({
                query(endpointName){
                    return `/noise/${endpointName}`;
                },
                providesTags: ['Noise']
            }),
            postNoise: builder.mutation<boolean, EndpointFieldPrefix & {isNoise: boolean}>({
                query({endpoint, fieldPrefix, isNoise}){
                    return {
                        url: `/noise/${endpoint}/prefix/${fieldPrefix}`,
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({isNoise})
                    }
                },
                invalidatesTags: ['Noise'],
            }),
            fetchEndpoints: builder.query<Map<string, EndpointMeta>, {excludeNoise:boolean, start:number, end:number}>({
                query({excludeNoise, start, end}){
                    return `/endpoints?exclude_noise=${excludeNoise}&start=${start}&end=${end}`;
                },
                providesTags: ['Noise']
            }),
            fetchFields: builder.query<Endpoint, FieldsQueryArgs>({
                query({selectedEndpoint, includeWeights, excludeNoise, start, end}){
                    return `/endpoints/${selectedEndpoint}/stats?include_weights=${includeWeights}&exclude_noise=${excludeNoise}&start=${start}&end=${end}`;
                },
                providesTags: ['Noise']
            }),
            fetchDifferences: builder.query<DifferenceResults, DifferencesQueryArgs>({
                query(args){
                    return `/endpoints/${args.selectedEndpoint}/fields/${args.selectedFieldPrefix}/results?include_weights=${args.includeWeights}&exclude_noise=${args.excludeNoise}&start=${args.start}&end=${args.end}`;
                },
                providesTags: ['Noise']
            })
        }
    },
});

export const {
    useFetchNoiseQuery,
    usePostNoiseMutation,
    useFetchEndpointsQuery,
    useFetchFieldsQuery,
    useFetchDifferencesQuery
} = apiNoiseSlice;