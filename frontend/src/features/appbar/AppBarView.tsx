import { AppBar, Toolbar, Typography, Tooltip, IconButton, Switch, Divider, Link } from "@mui/material"
import SettingsIcon from '@mui/icons-material/Settings';
import NotesIcon from '@mui/icons-material/Notes';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import JavascriptIcon from '@mui/icons-material/Javascript';

import DateTimeRangePicker from '@wojtekmaj/react-datetimerange-picker';
import '@wojtekmaj/react-datetimerange-picker/dist/DateTimeRangePicker.css';
import 'react-calendar/dist/Calendar.css';
import 'react-clock/dist/Clock.css';

import { fetchinfo } from "../info/infoApiSlice";
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { openInfoView, setDateTimeRange, toggleNoiseCancellation } from '../selections/selectionsSlice';
import { openOverrideView } from '../overrides/overrideSlice';

export default function AppBarView(){
  const info = fetchinfo();
  const excludeNoise = useAppSelector((state) => state.selections.noiseCancellationIsOn);
  const dateTimeRange = useAppSelector((state) => state.selections.dateTimeRange);
  const dispatch = useAppDispatch();
  return <AppBar position='static'>
    <Toolbar>
      <Typography variant="h6" color="inherit" sx={{ flexGrow: 1 }}>{info.name}</Typography>
      <Tooltip title="Analyze a time range">
        <IconButton>
          <DateTimeRangePicker 
            onChange={(range) => dispatch(setDateTimeRange(range))}
            value={dateTimeRange}
            disableClock={true}
          />
        </IconButton>
      </Tooltip>
      <Tooltip title={excludeNoise?"Show Noise":"Hide Noise"}>
        <IconButton color="inherit" aria-label="logs">
          <Switch
            color="secondary"
            checked={excludeNoise}
            onChange={()=> {dispatch(toggleNoiseCancellation())}}
          />
        </IconButton>
      </Tooltip>
      <Tooltip title='Mutate requests with Javascript'>
        <IconButton
          color="inherit"
          aria-label="mark"
          onClick={() => dispatch(openOverrideView())}>
          <JavascriptIcon/>
        </IconButton>
      </Tooltip>
      <Divider orientation="vertical" variant="middle" color="neutral" flexItem/>
      <Tooltip title="Logs">
        <Link color="inherit" target="_blank" href="http://localhost:3000/explore?orgId=1&left=%5B%22now-5m%22,%22now%22,%22Loki%22,%7B%22expr%22:%22%7Bjob%3D%5C%22diffy%5C%22%7D%22%7D%5D">
          <IconButton color="inherit" aria-label="logs"><NotesIcon/></IconButton>
        </Link>
      </Tooltip>
      <Tooltip title="Metrics">
        <Link color="inherit" target="_blank" href="http://localhost:9090/graph">
          <IconButton color="inherit" aria-label="metrics"><AnalyticsIcon/></IconButton>
        </Link>
      </Tooltip>
      <Tooltip title="Traces">
        <Link color="inherit" target="_blank" href="http://localhost:16686/search">
          <IconButton color="inherit" aria-label="traces"><AccountTreeIcon/></IconButton>
        </Link>
      </Tooltip>
      <Tooltip title="Settings">
        <IconButton
          color="inherit"
          aria-label="settings"
          edge="end"
          onClick={() => {dispatch(openInfoView())}}>
          <SettingsIcon/>
        </IconButton>
      </Tooltip>
    </Toolbar>
  </AppBar>
}