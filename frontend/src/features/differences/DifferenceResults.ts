export interface DifferenceResults {
    endpoint: string;
    path: string;
    requests: Request[];
}
interface Request {
    id: string;
    differences: Map<String, Difference[]>;
}
interface Difference {
    type: string;
    left: any;
    right: any;
}
