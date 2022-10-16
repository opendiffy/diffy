import React from 'react';
import TreeView from '@mui/lab/TreeView';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import TreeItem from '@mui/lab/TreeItem';

import _ from 'lodash';
import { Checkbox, ListItemText, Typography, Stack, ListItem, Tooltip } from '@mui/material';


class Metrics {
  constructor(obj){
    this.noise = obj.noise
    this.relative_difference = obj.relative_difference
    this.absolute_difference = obj.absolute_difference
    this.differences = obj.differences
    this.weight = obj.weight
  }
  message(){
    if(!this.differences){
      return `No differences observed.`
    }
    return `${this.differences} diffs | ${this.noise/this.differences*100.00}% noise`
  }
}
export class RecursiveTreeView extends React.Component{
  state = {
    isIgnored:{}
  }
nodeOf(obj, path, name){
  const id = path?`${path}.${name}` : name;
  return {
    id,
    name,
    children: this.childrenOf(obj, id),
    isIgnored: false
  }
}
childrenOf(obj, objName) {
  if(typeof obj !== 'object' || obj instanceof Metrics) {
    return []
  }
  return Object.keys(obj).map(name => this.nodeOf(obj[name], objName, name))
}
isIgnored(id){
  if(!id){
    return false;
  }
  const names = id.split('.').reverse()
  var root = names.pop()
  while (names.length > 0) {
    root = `${root}.${names.pop()}`
    if(this.state.isIgnored[root]){
      return true;
    }
  }
  return false;
}

label(nodes, tree){
  const metrics = _.get(tree, nodes.id)
  return <ListItemText
    primary={
      <Stack direction={'row'}>
        <Typography sx={{ flexGrow : 1 }}>{nodes.name}</Typography>
        <Tooltip title={'Ignore'}>
          <Checkbox
            checked={this.props.isIgnored(nodes.id)}
            onClick={() => {this.props.toggleIgnore(nodes.id)}}
          />
        </Tooltip>
      </Stack>
    }
    secondary={typeof metrics.message === 'function' ? metrics.message() : undefined}/>
}
renderTree(nodes, setFieldPrefix, tree) {
    return <TreeItem
      key={nodes.id}
      nodeId={nodes.id}
      label={this.label(nodes, tree)}
      onClick={() => setFieldPrefix(nodes.id)}
    >
      {Array.isArray(nodes.children) ? nodes.children.map((node) => this.renderTree(node, setFieldPrefix, tree)) : null}
    </TreeItem>
}


render() {
  const tree = {}
  Object.keys(this.props.fields).forEach(path => {
    const metrics = new Metrics(this.props.fields[path])
    const included = metrics.differences > metrics.noise && metrics.relative_difference > this.props.relativeThreshold && metrics.absolute_difference > this.props.absoluteThreshold;
    if(included && !this.props.isIgnored(path) || !this.props.ignoreNoise){
      _.update(tree, path, () => metrics)
    }
  })

  return <TreeView
    defaultCollapseIcon={<ExpandMoreIcon />}
    defaultExpanded={['result', 'result.200 OK']}
    defaultExpandIcon={<ChevronRightIcon />}
  >
    {this.childrenOf(tree, '').map(child =>
      this.renderTree(child, this.props.setFieldPrefix, tree)
    )}
  </TreeView>;
}
}