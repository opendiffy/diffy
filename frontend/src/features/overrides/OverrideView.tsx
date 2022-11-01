import { Grid, Tooltip, Typography } from '@mui/material';

import { useAppDispatch, useAppSelector } from '../../app/hooks'

import { highlightEdge, selectEdge} from './overrideSlice';
import CodeEditor from './editor/CodeEditor';

export function OverrideView(){
  const dispatch = useAppDispatch();
  const highlightedEdge = useAppSelector((state) => state.overrides.highlightedEdge);
  const selectedEdge = useAppSelector((state) => state.overrides.selectedEdge);

  function isHighlighted(edge: string): boolean {
    return highlightedEdge === edge || highlightedEdge === 'all' || isSelected(edge);
  }
  function isSelected(edge: string): boolean {
    return selectedEdge === edge || selectedEdge === 'all';
  }

  const radius = 20
  const delta = {x: 100, y:100}
  type Coord = {x: number,y:number}
  type Edge = {start:Coord, end:Coord}
  const all = {start:{x:0,y:200}, end:{x:delta.x, y:200}}
  const candidate = {start: all.end, end: {x: all.end.x+delta.x, y: all.end.y-delta.y}}
  const primary = {start: all.end, end: {x: all.end.x+delta.x, y: all.end.y}}
  const secondary = {start: all.end, end: {x: all.end.x+delta.x, y: all.end.y+delta.y}}

  function drawEdge(name: string, edge: Edge) {
    return <><Tooltip
    title={`Apply to ${name}`}
    onMouseOver={()=>{dispatch(highlightEdge(name))}}
    onMouseOut={()=>{dispatch(highlightEdge(undefined))}}
    onClick={() => dispatch(selectEdge(name))}
    >
    <line
      x1={edge.start.x}
      y1={edge.start.y}
      x2={edge.end.x}
      y2={edge.end.y}
      stroke='currentColor' strokeWidth={isHighlighted(name)? "4": "2"} />
      </Tooltip>
      <Tooltip
      title={`Apply to ${name}`}
      onMouseOver={()=>{dispatch(highlightEdge(name))}}
      onMouseOut={()=>{dispatch(highlightEdge(undefined))}}
      onClick={() => dispatch(selectEdge(name))}
      >
    <circle
      cx={edge.end.x}
      cy={edge.end.y}
      r={radius} stroke="black" fill="blue" strokeWidth={isHighlighted(name)? "4": "0"}/>
    </Tooltip>
    <text
      x={edge.end.x - radius}
      y={edge.end.y + 2 * radius}
      fill='black'>{name}</text></>;
  }
return     <Grid container>
    <Grid item xs={4}>
    <Typography>Request Pattern</Typography>
    <svg height="400" width="400">
      {drawEdge('candidate', candidate)}
      {drawEdge('primary', primary)}
      {drawEdge('secondary', secondary)}
      {drawEdge('all', all)}
    </svg>
    </Grid>
    {!selectedEdge
        ? <Grid item xs={8}></Grid>
        : 
    <Grid item xs={8}>
      <Typography>Overriding traffic to <strong>{selectedEdge}</strong> with the following function:</Typography>
      <CodeEditor/>
    </Grid>
        }
  </Grid>
}