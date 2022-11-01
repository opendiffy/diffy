import { NamedMetric, isNamedMetric } from "./Metric";

export interface FieldNode {
    id: string,
    name: string,
    children: FieldNode[],
    namedMetric: NamedMetric|undefined
}

export function nodeOf(obj: any, path: string, name: string): FieldNode {
    const id = path?`${path}.${name}` : name;
    const children = childrenOf(obj, id);
    const namedMetric = children.length ? undefined : obj as NamedMetric;
    return {
      id,
      name,
      children,
      namedMetric
    }
}

function childrenOf(obj: any, objName: string): FieldNode[] {
    if(typeof obj !== 'object' || isNamedMetric(obj)) {
        return [];
    }
    return Object.keys(obj).map(name => nodeOf(obj[name], objName, name));
}