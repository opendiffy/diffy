import { Metric, isMetric } from "./Metric";

export interface FieldNode {
    id: string,
    name: string,
    children: FieldNode[],
    metric: Metric|undefined
}

export function nodeOf(obj: any, path: string, name: string): FieldNode {
    const id = path?`${path}.${name}` : name;
    const children = childrenOf(obj, id);
    const metric = children.length ? undefined : obj as Metric;
    return {
      id,
      name,
      children,
      metric
    }
}

function childrenOf(obj: any, objName: string): FieldNode[] {
    if(typeof obj !== 'object' || isMetric(obj)) {
        return [];
    }
    return Object.keys(obj).map(name => nodeOf(obj[name], objName, name));
}