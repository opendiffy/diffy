import _ from 'lodash';

import { List, ListItem, ListItemText, ListSubheader, Switch, Stack, Tooltip, Typography, IconButton} from '@mui/material';
import {TreeView, TreeItem} from '@mui/lab';
import {ExpandMore, ChevronRight} from '@mui/icons-material';

import { useAppSelector, useAppDispatch } from '../../app/hooks';
import { selectFieldPrefix } from '../selections/selectionsSlice';
import { FieldNode, nodeOf } from './FieldNode';
import { isMetric, Metric } from './Metric';
import { useFetchNoiseQuery, usePostNoiseMutation, useFetchFieldsQuery } from '../noise/noiseApiSlice';


export function FieldsView(){
    const dispatch = useAppDispatch();
    const excludeNoise = useAppSelector((state) => state.selections.noiseCancellationIsOn);
    const selectedEndpoint = useAppSelector((state) => state.selections.endpointName) || 'unknown';
    const endpoint = useFetchFieldsQuery({selectedEndpoint, excludeNoise, includeWeights: true}, {pollingInterval: 10*1000}).data || {fields: new Map<string, Metric>()};
    const noisyPrefixes = useFetchNoiseQuery(selectedEndpoint).data || [];
    const [updateNoise, { isLoading: isUpdating }] = usePostNoiseMutation();
    if(!endpoint || !endpoint.fields || !Object.keys(endpoint.fields).length){
        return <List subheader={<ListSubheader>Fields</ListSubheader>}><ListItem><ListItemText>No differences.</ListItemText></ListItem></List>;
    }
    const fields = endpoint.fields;
    const tree = {};
    Object.entries(fields).forEach(([field, metric]) => {
        _.update(tree, field, () => metric);
    })
    const children = nodeOf(tree,'','').children // discard the root and render its children
    let disabledNode = ''
    function renderTree(node: FieldNode, tree: any) {
        return <TreeItem
          key={node.id}
          nodeId={node.id}
          label={label(selectedEndpoint, node, tree)}
          onClick={() => dispatch(selectFieldPrefix(node.id))}
          disabled={ disabledNode === node.id }
        >
          {node.children.map((child) => renderTree(child, tree))}
        </TreeItem>
    }
    function label(endpoint: string, node: FieldNode, tree: any){
        const metrics = _.get(tree, node.id)
        const fieldPrefix = node.id;
        const checked = isDirectlyMarkedAsNoise(fieldPrefix);
        return <ListItemText
          primary={
            <Stack direction={'row'} height={30}>
              <Typography sx={{ flexGrow : 1 }}>{node.name}</Typography>
              <Tooltip title={!checked?'Ignore':'Include'}>
                <IconButton color="inherit" aria-label="logs">
                  <Switch
                    checked={checked}
                    disabled={(isNoise(fieldPrefix) && !checked) || isUpdating}
                    onChange={() => updateNoise({endpoint, fieldPrefix, isNoise: !checked})}
                    onMouseEnter={()=>{disabledNode = node.id}} onMouseLeave={()=>{disabledNode=''}}
                  />
                </IconButton>
              </Tooltip>
            </Stack>
          }
          secondary={message(metrics)}/>
    }
    function isNoise(prefix: string) {
        for(let i in noisyPrefixes){
            if(prefix.startsWith(noisyPrefixes[i])){
                return true;
            }
        }
        return false;
    }
    function isDirectlyMarkedAsNoise(prefix:string){
        return noisyPrefixes.includes(prefix);
    }
    function   message(metric: Metric){
        if(!isMetric(metric)){
            return undefined;
        }
        if(!metric.differences){
          return `No differences observed.`
        }
        return `${metric.differences} diffs | ${metric.noise/metric.differences*100.00}% noise`
      }
    return       (<List subheader={<ListSubheader>Fields</ListSubheader>}>
    <TreeView
      defaultCollapseIcon={<ExpandMore />}
      defaultExpanded={['result', 'result.200 OK']}
      defaultExpandIcon={<ChevronRight />}
    >
      {children.map(child => renderTree(child, tree))}
    </TreeView>
  </List>);
}