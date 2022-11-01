import { Dialog, DialogContent, Grid } from '@mui/material';
import 'bootstrap/dist/css/bootstrap.min.css';

import { useAppDispatch, useAppSelector } from './app/hooks'
import InfoView from './features/info/InfoView';
import EndpointsView from './features/endpoints/EndpointsView';
import AppBarView from "./features/appbar/AppBarView";
import { FieldsView } from './features/fields/FieldsView';
import { RequestView } from './features/requests/RequestView';
import { closeInfoView, closeRequestView } from './features/selections/selectionsSlice';
import { closeOverrideView } from './features/overrides/overrideSlice';
import { DifferencesView } from './features/differences/DifferencesView';
import { OverrideView } from './features/overrides/OverrideView';

function App() {
  const dispatch = useAppDispatch();
  const infoIsOpen = useAppSelector((state) => state.selections.infoIsOpen);
  const requestIsOpen = useAppSelector((state) => state.selections.requestIsOpen);
  const overrideViewIsOpen = useAppSelector((state) => state.overrides.overrideViewIsOpen);

  return (
    <Grid container>
    <Grid item xs={12}>
      <AppBarView/>
    </Grid>
    <Grid item xs={3}>
      <EndpointsView/>
    </Grid>
    <Grid item xs={4}>
      <FieldsView/>
    </Grid>
    <Grid item xs={5}>
      <DifferencesView/>
    </Grid>

    <Dialog
      fullWidth
      maxWidth='md'
      open={infoIsOpen}
      onClose={()=>{dispatch(closeInfoView())}}>
      <DialogContent>
        <InfoView/>
      </DialogContent>
    </Dialog>
    
    <Dialog
      fullWidth
      maxWidth='md'
      open={requestIsOpen}
      onClose={()=>{dispatch(closeRequestView())}}>
      <DialogContent>
        <RequestView/>
      </DialogContent>
    </Dialog>

    <Dialog
      fullWidth
      maxWidth='md'
      open={overrideViewIsOpen}
      onClose={()=>{dispatch(closeOverrideView())}}>
      <DialogContent>
        <OverrideView/>
      </DialogContent>
    </Dialog>
    
  </Grid>
  )
}

export default App
