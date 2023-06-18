import _ from 'lodash';

import { List, ListItem, ListItemText, ListSubheader, Switch, Stack, Tooltip, Typography, IconButton} from '@mui/material';
import {TreeView, TreeItem} from '@mui/lab';
import {ExpandMore, ChevronRight} from '@mui/icons-material';

import { useAppSelector, useAppDispatch } from '../../app/hooks';
import { selectFieldPrefix } from '../selections/selectionsSlice';
import { FieldNode, nodeOf } from './FieldNode';
import { isNamedMetric, Metric, NamedMetric } from './Metric';
import { useFetchNoiseQuery, usePostNoiseMutation, useFetchFieldsQuery } from '../noise/noiseApiSlice';


export function FieldsView(){
    const dispatch = useAppDispatch();
    const excludeNoise = useAppSelector((state) => state.selections.noiseCancellationIsOn);
    const selectedEndpoint = useAppSelector((state) => state.selections.endpointName) || 'unknown';
    const [start, end] = useAppSelector((state) => state.selections.dateTimeRange);
    const endpoint = useFetchFieldsQuery({selectedEndpoint, excludeNoise, includeWeights: true, start, end}).data || {fields: new Map<string, Metric>()};
    const noisyPrefixes = useFetchNoiseQuery(selectedEndpoint).data || [];
    const [updateNoise, { isLoading: isUpdating }] = usePostNoiseMutation();
    if(!endpoint || !endpoint.fields || !Object.keys(endpoint.fields).length){
        return <List subheader={<ListSubheader>Fields</ListSubheader>}><ListItem><ListItemText>No differences.</ListItemText></ListItem></List>;
    }
    const fields = endpoint.fields;
    const tree = {request:{}, result:{}};
    Object.entries(fields).forEach(([field, metric]) => {
      const tokens = field.split('.');
      const name = tokens.pop();
      const namedMetric : NamedMetric = {...metric, name};
        _.update(tree, tokens.join('.'), () => namedMetric);
    })
    const children = nodeOf(tree,'','').children // discard the root and render its children
    const request = children.find(child => child.name === 'request')
    const response = children.find(child => child.name === 'response')
    let disabledNode = ''
    function renderTree(node: FieldNode, tree: any, noDiff: boolean) {
        return <TreeItem
          key={node.id}
          nodeId={node.id}
          label={label(selectedEndpoint, node, tree, noDiff)}
          onClick={() => dispatch(selectFieldPrefix(node.id))}
          disabled={ disabledNode === node.id }
        >
          {node.children.map((child) => renderTree(child, tree, noDiff))}
        </TreeItem>
    }
    function label(endpoint: string, node: FieldNode, tree: any, noDiff: boolean){
        const metrics = _.get(tree, node.id)
        const isNoDifference = metrics && metrics.name === 'NoDifference';
        const fieldPrefix = node.id;
        const checked = isDirectlyMarkedAsNoise(fieldPrefix);
        return <ListItemText
          primary={
            <Stack direction={'row'} height={30}>
              <Typography sx={{ flexGrow : 1 }}>{node.name}</Typography>
              <Tooltip title={!checked?'Ignore':'Include'} hidden={isNoDifference || noDiff}>
                <IconButton color="inherit" aria-label="mark">
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
          secondary={message(metrics, noDiff)}/>
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
    function   message(metric: NamedMetric, noDiff: boolean){
        if(!isNamedMetric(metric) || noDiff){
            return undefined;
        }
        if(!metric.differences || metric.name === 'NoDifference'){
          return `No differences observed.`
        }
        return `${metric.name}: ${metric.differences} diffs | ${metric.noise/(metric.differences)*100.00}% noise/diffs`
      }
    return       (<List subheader={<ListSubheader>Fields</ListSubheader>}>
          <TreeView
      defaultCollapseIcon={<ExpandMore />}
      defaultExpanded={['request']}
      defaultExpandIcon={<ChevronRight />}
    >
      {request && renderTree(request, tree, true)}
    </TreeView>
    <TreeView
      defaultCollapseIcon={<ExpandMore />}
      defaultExpanded={['response']}
      defaultExpandIcon={<ChevronRight />}
    >
      {response && renderTree(response, tree, false)}
    </TreeView>
  </List>);
}