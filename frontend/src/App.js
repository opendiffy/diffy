import React from 'react';
import {AppBar, Dialog, DialogContent, Grid, IconButton, Link, List, ListItem, ListItemText, ListSubheader, Toolbar, Tooltip, Typography} from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import NotesIcon from '@mui/icons-material/Notes';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import AccountTreeIcon from '@mui/icons-material/AccountTree';

import logo from './logo.svg';
import './App.css';
import { RecursiveTreeView } from './RecursiveTreeView';

const target = ""
class App extends React.Component {
  state = {
    dialog: false,
    info : {
      primary: {target},
      secondary: {target},
      candidate: {target},
      relativeThreshold: 20,
      last_reset: 0,
      absoluteThreshold: 0.03,
      protocol: "http",
    },
    endpoints: {},
    selectedEndpoint: false,
    endpoint: {
      endpoint:{},
      fields:[]
    },
    field: {
      requests:[]
    },
    request:{
      request:{},
      left:{},
      right:{}
    }
  };
  api = {
    info : '/api/1/info',
    endpoints : '/api/1/info'
  }
  async componentDidMount() {
    setInterval(() => {
      fetch('/api/1/endpoints')
      .then(response => response.json())
      .then(endpoints => this.setState({...this.state, endpoints}));

      if(this.state.selectedEndpoint){
        fetch(`/api/1/endpoints/${this.state.selectedEndpoint}/stats?include_weights=true&exclude_noise=false`)
        .then(response => response.json())
        .then(endpoint => this.setState({...this.state, endpoint}));
      }

      if(this.state.selectedField){
        fetch(`/api/1/endpoints/${this.state.selectedEndpoint}/fields/${this.state.selectedField}/results`)
        .then(response => response.json())
        .then(field => this.setState({...this.state, field}));
      }
    }, 2000);
    fetch('/api/1/info')
      .then(response => response.json())
      .then(info => this.setState({...this.state, info}));
  }
  fetchRequest(id){
    fetch(`/api/1/requests/${id}`)
    .then(response => response.json())
    .then(request => this.setState({...this.state, request, requestOpen:true}));
  }
  render() {
    const {info, endpoints, endpoint, field, request} = this.state;
    return (
<Grid container>
  <Grid item xs={12}>
    <AppBar position='static'>
      <Toolbar>
        <Typography variant="h6" color="inherit" sx={{ flexGrow: 1 }}>Diffy</Typography>
        <Tooltip title="Logs">
          <Link color="inherit" target="_blank" href="http://localhost:3000/explore">
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
            onClick={() => {this.setState({...this.state, dialog: true})}}>
            <SettingsIcon/>
          </IconButton>
        </Tooltip>
      </Toolbar>
    </AppBar>
  </Grid>
  <Grid item xs={3}>
    <List subheader={<ListSubheader>Endpoints</ListSubheader>}>
      {Object.keys(endpoints).map((name, i) => {
          const {total, differences} = endpoints[name]
          return <ListItem onClick={() => {this.setState({...this.state, selectedEndpoint: name})}}>
            <ListItemText primary={name} secondary={`${differences} failing of ${total} requests`}/>
          </ListItem>
          })}
    </List>
  </Grid>
  <Grid item xs={4}>

    <List subheader={<ListSubheader>Fields</ListSubheader>}>
    <RecursiveTreeView
      fields = {endpoint.fields}
      // fields = {{
      //   "a.b.c":{noise:0,relative_difference:0,absolute_difference:0,differences:0,weight:0},
      //   "a.b.d":{noise:0,relative_difference:0,absolute_difference:0,differences:0,weight:0},
      //   "a.d.c":{noise:0,relative_difference:0,absolute_difference:0,differences:0,weight:0},
      //   "a.d.d":{noise:0,relative_difference:0,absolute_difference:0,differences:0,weight:0},
      // }}
      setFieldPrefix = {(prefix) => this.setState({...this.state, selectedField: prefix})}
    />
      {Object.keys(endpoint.fields).map((field) => {
          const {noise,relative_difference,absolute_difference,differences,weight} = endpoint.fields[field];
          return <ListItem onClick={() => {this.setState({...this.state, selectedField: field})}}>
            <ListItemText primary={field} secondary={`${differences} diffs ${noise} noise`}/>
          </ListItem>
          })}
    </List>
  </Grid>
  <Grid item xs={5}>
    <List subheader={<ListSubheader>Results</ListSubheader>}>
      {field.requests.map((request) => {
        const {id} = request
        const {type, left, right} = request.differences[this.state.selectedField]
          return <ListItem onClick={() => {this.fetchRequest(id)}}>
            <ListItemText primary={type} secondary={`${left} ${right}`}/>
          </ListItem>
          })}
    </List>
  </Grid>
  <Dialog open={!!this.state.dialog} onClose={()=>{this.setState({...this.state, dialog: false})}}>
    <DialogContent>
    <Grid container>
      <Grid item sx={6}>
        <Typography>Candidate Server</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>{info.candidate.target}</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>Primary Server</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>{info.primary.target}</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>Secondary Server</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>{info.secondary.target}</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>Protocol</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>{info.protocol}</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>Last Reset</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>{new Date(info.last_reset).toDateString}</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography>Thresholds</Typography>
      </Grid>
      <Grid item sx={6}>
        <Typography><strong>{info.relativeThreshold}%</strong> relative, <strong>{info.absoluteThreshold}%</strong> absolute</Typography>
      </Grid>
    </Grid>
    </DialogContent>
  </Dialog>
  <Dialog open={!!this.state.requestOpen} onClose={()=>{this.setState({...this.state, requestOpen: false})}}>
    <Grid container>
      <DialogContent>
        <Grid item sx={12}>Request</Grid>
        <Grid item sx={12}>{JSON.stringify(request.request,null,4)}</Grid>
        <Grid item sx={6}>Primary</Grid>
        <Grid item sx={6}>Candidate</Grid>
      </DialogContent>
    </Grid>
  </Dialog>
</Grid>);
  }
}

export default App;
