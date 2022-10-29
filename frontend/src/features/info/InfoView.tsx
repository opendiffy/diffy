import DeleteIcon from '@mui/icons-material/Delete';
import { Alert, Grid, IconButton, Snackbar, Tooltip, Typography } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../../app/hooks'

import { fetchinfo, useDeleteRequestsMutation } from './infoApiSlice';
import { openDeleteRequestsAlert, closeDeleteRequestsAlert } from '../selections/selectionsSlice';

export default function InfoView(){
  const dispatch = useAppDispatch();
  const info = fetchinfo();
  const alertIsOpen = useAppSelector((state) => state.selections.deleteRequestAlertIsOpen);
  const closeAlert = () => dispatch(closeDeleteRequestsAlert());
  const [deleteRequests, { isLoading: isUpdating, isSuccess }] = useDeleteRequestsMutation();

    return     <Grid container>
    <Grid item xs={6}>
      <Typography>Candidate Server</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>{info.candidate.target}</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>Primary Server</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>{info.primary.target}</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>Secondary Server</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>{info.secondary.target}</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>Protocol</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>{info.protocol}</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>Last Reset</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>{new Date(info.last_reset).toDateString()}</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography>Thresholds</Typography>
    </Grid>
    <Grid item xs={6}>
      <Typography><strong>{info.relativeThreshold}%</strong> relative, <strong>{info.absoluteThreshold}%</strong> absolute</Typography>
    </Grid>
    <Grid item xs={3}>
      <Tooltip title="Delete all requests">
        <IconButton
          color="inherit"
          aria-label="delete"
          onClick={()=>deleteRequests().then(() => dispatch(openDeleteRequestsAlert()))}>
          <DeleteIcon color="inherit"/>
        </IconButton>
      </Tooltip>
    </Grid>
    <Grid item xs={9}>
      <Snackbar open={alertIsOpen} autoHideDuration={6000} onClose={closeAlert}>
        <Alert onClose={closeAlert} severity="success" sx={{ width: '100%' }}>
          All requests deleted!
        </Alert>
      </Snackbar>
    </Grid>
  </Grid>
}