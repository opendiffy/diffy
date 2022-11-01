import { configureStore } from "@reduxjs/toolkit";
import selectionsReducer from '../features/selections/selectionsSlice';
import overridesReducer from '../features/overrides/overrideSlice';
import { apiInfoSlice } from "../features/info/infoApiSlice";
import { apiRequestsSlice } from "../features/requests/requestsApiSlice";
import { apiNoiseSlice } from "../features/noise/noiseApiSlice";
import { apiTransformationsSlice } from "../features/overrides/transformationsApiSlice";

export const store = configureStore({
    reducer: {
        selections: selectionsReducer,
        overrides: overridesReducer,
        [apiInfoSlice.reducerPath]: apiInfoSlice.reducer,
        [apiRequestsSlice.reducerPath]: apiRequestsSlice.reducer,
        [apiNoiseSlice.reducerPath]: apiNoiseSlice.reducer,
        [apiTransformationsSlice.reducerPath]: apiTransformationsSlice.reducer
    },
    middleware: (getDefaultMiddleware) => {
        return getDefaultMiddleware()
        .concat(apiInfoSlice.middleware)
        .concat(apiRequestsSlice.middleware)
        .concat(apiNoiseSlice.middleware)
        .concat(apiTransformationsSlice.middleware);
    }
});

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>;