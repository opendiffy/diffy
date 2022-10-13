import React from 'react';
import TreeView from '@mui/lab/TreeView';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import TreeItem from '@mui/lab/TreeItem';

import _ from 'lodash';

class Metrics{
  Metrics(obj){
    this.noise = obj.noise
    this.relative_difference = obj.relative_difference
    this.absolute_difference = obj.absolute_difference
    this.differences = obj.differences
    this.weight = obj.weight
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

function renderTree(nodes, props, state) {
  if(nodes.children.length === 0) {
    return <TreeItem
      key={nodes.id}
      nodeId={nodes.id}
      label={nodes.name}
      onClick={() => props.setFieldPrefix(nodes.id)}
    />
  }
  return <TreeItem key={nodes.id} nodeId={nodes.id} label={nodes.name} onClick={() => {}}>
    {Array.isArray(nodes.children) ? nodes.children.map((node) => renderTree(node, props, state)) : null}
  </TreeItem>
}
export class RecursiveTreeView extends React.Component {
  constructor(props){
    super(props);
    const {fields} = props;
    this.state = {}
    Object.keys(fields).forEach(path => {
      _.update(this.state, path, () => new Metrics(fields[path]))
    })
    
  }

  render() {
    delete this.state.downstream;
    return <TreeView
      // className={this.props.classes.root}
      defaultCollapseIcon={<ExpandMoreIcon />}
      defaultExpanded={[]}
      defaultExpandIcon={<ChevronRightIcon />}
    >
      {childrenOf(this.state, '').map(child =>
        renderTree(child, this.props, this.state)
      )}
    </TreeView>
  }
}