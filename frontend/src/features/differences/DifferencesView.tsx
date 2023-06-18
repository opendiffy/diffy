import {IconButton, keyframes, List, ListItem, ListItemText, ListSubheader, Table, TableBody,TableCell, TableHead, TableRow } from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';

import { useAppDispatch, useAppSelector } from '../../app/hooks';
import { useFetchDifferencesQuery } from '../noise/noiseApiSlice';
import { DifferenceResults } from './DifferenceResults';
import { openRequestView, selectRequest } from '../selections/selectionsSlice';

export function DifferencesView(){
  const dispatch = useAppDispatch();
  const excludeNoise = useAppSelector((state) => state.selections.noiseCancellationIsOn);
  const selectedEndpoint = useAppSelector((state) => state.selections.endpointName) || 'unknown';
  const selectedFieldPrefix = useAppSelector((state) => state.selections.fieldPrefix) || 'unknown';
  const [start, end] = useAppSelector((state) => state.selections.dateTimeRange);
  const differenceResults = useFetchDifferencesQuery({excludeNoise, selectedEndpoint, selectedFieldPrefix, includeWeights:true, start, end}).data || {endpoint:'undefined', path:'undefined', requests:[]};
  if(!differenceResults.requests.length) {
    return <List subheader={<ListSubheader>Differences</ListSubheader>}>
      <ListItem><ListItemText>No differences.</ListItemText></ListItem>
    </List>
  }
  const {requests} = (differenceResults as DifferenceResults);
  const differences = requests.flatMap((request) => {
    const requestId = request.id
    return Object.entries(request.differences).flatMap(([key, diff]) => {
      if(!key.startsWith(selectedFieldPrefix)){
        return [];
      }
      const {type, left, right} = diff
      return [{requestId, type, left, right, key:`${selectedEndpoint}.${key}.${requestId}`}];
    });
  });
  return <List subheader={<ListSubheader>Differences</ListSubheader>}>
    <Table>
      <TableHead>
        <TableRow>
          <TableCell>{'Type'}</TableCell>
          <TableCell>{'Expected'}</TableCell>
          <TableCell>{'Actual'}</TableCell>
          <TableCell>{'Details'}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
      {
        differences.map(({requestId, type, left, right, key}) => {
        return <TableRow key={key}>
          <TableCell>{type}</TableCell>
          <TableCell>{left}</TableCell>
          <TableCell>{right}</TableCell>
          <TableCell>
            <IconButton
              onClick={() => {
                dispatch(selectRequest(requestId));
                dispatch(openRequestView());
              }}>
            <OpenInNewIcon/>
            </IconButton>
          </TableCell>
        </TableRow>;
      })}
      </TableBody>
    </Table>
    </List>;
}