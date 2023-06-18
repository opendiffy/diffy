import { List, ListItem, ListItemButton, ListItemText, ListSubheader } from '@mui/material';

import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { EndpointMeta } from "./EndpointMeta";
import { selectEndpoint } from '../selections/selectionsSlice';
import { useFetchNoiseQuery, useFetchEndpointsQuery } from '../noise/noiseApiSlice';

export default function EndpointsView() {
  const dispatch = useAppDispatch();
  const excludeNoise = useAppSelector((state) => state.selections.noiseCancellationIsOn);
  const selectedEndpoint = useAppSelector((state) => state.selections.endpointName) || 'undefined';
  const {start, end} = useAppSelector((state) => state.selections.dateTimeRange);
  const noisyFields = useFetchNoiseQuery(selectedEndpoint).data || [];
  const endpoints: Map<string, EndpointMeta> = useFetchEndpointsQuery({excludeNoise, start, end}).data || new Map<string, EndpointMeta>();  
  const entries = Object.entries(endpoints);
  return (<List subheader={<ListSubheader>Endpoints</ListSubheader>}>
    {!entries.length?<ListItem><ListItemText>No endpoints.{excludeNoise && noisyFields.length?'(Some ignored)':''}</ListItemText></ListItem>:
      entries.map(([name, {total, differences}]) => {
        return <ListItemButton key={name} selected={name === selectedEndpoint} onClick={() => {dispatch(selectEndpoint(name))}}>
          <ListItemText primary={name} secondary={`${differences} failing of ${total} requests`}/>
        </ListItemButton>
        })}
  </List>);
}