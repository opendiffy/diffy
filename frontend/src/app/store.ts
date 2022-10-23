import { configureStore } from "@reduxjs/toolkit";
import selectionsReducer from '../features/selections/selectionsSlice';

import { apiInfoSlice } from "../features/info/infoApiSlice";
import { apiRequestsSlice } from "../features/requests/requestsApiSlice";
import { apiNoiseSlice } from "../features/noise/noiseApiSlice";

export const store = configureStore({
    reducer: {
        selections: selectionsReducer,
        [apiInfoSlice.reducerPath]: apiInfoSlice.reducer,
        [apiRequestsSlice.reducerPath]: apiRequestsSlice.reducer,
        [apiNoiseSlice.reducerPath]: apiNoiseSlice.reducer
    },
    middleware: (getDefaultMiddleware) => {
        return getDefaultMiddleware()
        .concat(apiInfoSlice.middleware)
        .concat(apiRequestsSlice.middleware)
        .concat(apiNoiseSlice.middleware);
    }
});

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>;