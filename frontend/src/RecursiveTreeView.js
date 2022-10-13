import React from 'react';
import TreeView from '@mui/lab/TreeView';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import TreeItem from '@mui/lab/TreeItem';

import _ from 'lodash';
import { ListItemText, Typography } from '@mui/material';

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
function childrenOf(obj, objName) {
  if(typeof obj !== 'object' || obj instanceof Metrics) {
    return []
  }
  const result = Object.keys(obj).map(name => {
    const path = objName?`${objName}.${name}` : name;
    return {
      id : path,
      name,
      children : childrenOf(obj[name], path),
    }
  })
  return result;
}

function renderTree(nodes, setFieldPrefix, tree) {
  if(nodes.children.length === 0) {
    const metrics = _.get(tree, nodes.id)
    return <TreeItem
      key={nodes.id}
      nodeId={nodes.id}
      label={<ListItemText primary={nodes.name} secondary={metrics.message()}/>}
      onClick={() => setFieldPrefix(nodes.id)}
    />
  }
  return <TreeItem key={nodes.id} nodeId={nodes.id} label={nodes.name} onClick={() => {}}>
    {Array.isArray(nodes.children) ? nodes.children.map((node) => renderTree(node, setFieldPrefix, tree)) : null}
  </TreeItem>
}


export function createTreeView(fields, setFieldPrefix) {
  const tree = {}
  Object.keys(fields).forEach(path => {
    _.update(tree, path, () => new Metrics(fields[path]))
  })

  return <TreeView
    defaultCollapseIcon={<ExpandMoreIcon />}
    defaultExpanded={['result', 'result.200 OK']}
    defaultExpandIcon={<ChevronRightIcon />}
  >
    {childrenOf(tree, '').map(child =>
      renderTree(child, setFieldPrefix, tree)
    )}
  </TreeView>;
}