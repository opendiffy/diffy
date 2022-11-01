import {createSlice, PayloadAction} from '@reduxjs/toolkit';
interface OverrideSelections {
    overrideViewIsOpen: boolean, // Override field with Javascript function
    alertIsOpen: string|undefined,
    highlightedEdge: string|undefined
    selectedEdge: string|undefined,
    currentTransformationJs: string| undefined
}
const initialState: OverrideSelections = {
    overrideViewIsOpen: false,
    alertIsOpen: undefined,
    highlightedEdge: undefined,
    selectedEdge: undefined,
    currentTransformationJs: undefined
};
const slice = createSlice({
    name: 'overrides',
    initialState,
    reducers: {
        openOverrideView(state){
            state.overrideViewIsOpen = true;
        },
        closeOverrideView(state){
            state.overrideViewIsOpen = false;
            state.alertIsOpen = undefined;
            state.highlightedEdge = undefined;
            state.selectedEdge = undefined;
            state.currentTransformationJs = undefined;
        },
        clearSelections(state){
            state.alertIsOpen = undefined;
            state.highlightedEdge = undefined;
            state.selectedEdge = undefined;
            state.currentTransformationJs = undefined;
        },
        openAlert(state, message){
            state.alertIsOpen = message.payload;
        },
        closeAlert(state){
            state.alertIsOpen = undefined;
        },
        highlightEdge(state, edge) {
            state.highlightedEdge = edge.payload;
        },
        selectEdge(state, edge) {
            state.selectedEdge = edge.payload;
        },
        setTransformationJs(state, transformationJs){
            state.currentTransformationJs = transformationJs.payload;
        }
    }
})

export const {
    openOverrideView,
    closeOverrideView,
    openAlert,
    closeAlert,
    highlightEdge,
    selectEdge,
    setTransformationJs
} = slice.actions;
export default slice.reducer;