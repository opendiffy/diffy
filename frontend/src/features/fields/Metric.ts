export interface Metric {
    noise: number;
    relative_difference: number;
    absolute_difference: number;
    differences: number;
    weight: number;
}

const zero: Metric = {
    noise: 0,
    relative_difference: 0,
    absolute_difference: 0,
    differences: 0,
    weight: 0
}

const metricKeys: Set<string> = new Set(Object.keys(zero))

export function isMetric(obj: any): obj is Metric {
    if(!obj){
        return false;
    }
    const objKeys = Object.keys(obj)
    return metricKeys.size === objKeys.length && objKeys.map(x => metricKeys.has(x)).reduce((acc, x) => acc && x);
}