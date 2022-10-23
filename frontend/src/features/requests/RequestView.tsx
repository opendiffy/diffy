import {ListItem, ListItemText, Table, TableBody,TableCell, TableContainer, TableHead, TableRow } from '@mui/material';
import { useAppSelector } from '../../app/hooks';
import { useFetchRequestQuery } from './requestsApiSlice';

export function RequestView(){
    const requestId = useAppSelector((state) => state.selections.requestId)
    const request = requestId && useFetchRequestQuery(requestId as string).data;

    return (!request?<ListItem><ListItemText>No requests.</ListItemText></ListItem>:<TableContainer>
    <Table>
    <TableHead><TableRow><TableCell>Request</TableCell></TableRow></TableHead>
    <TableBody><TableRow><TableCell>{<pre>{JSON.stringify(request.request,null,4)}</pre>}</TableCell></TableRow></TableBody>
    </Table>
    <Table>
    <TableHead>
    <TableRow><TableCell>Primary</TableCell><TableCell>Candidate</TableCell></TableRow>
    </TableHead>
    <TableBody>
    <TableRow>
    <TableCell>{<pre>{JSON.stringify(request.left,null,4)}</pre>}</TableCell>
    <TableCell>{<pre>{JSON.stringify(request.right,null,4)}</pre>}</TableCell>
    </TableRow>
    </TableBody>
    </Table>
    </TableContainer>);
}