import { Grid, Typography } from '@mui/material';
import { fetchinfo } from './infoApiSlice';

export default function InfoView(){
  const target = 'Unknown';
  const info = fetchinfo();
  
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
  </Grid>
}