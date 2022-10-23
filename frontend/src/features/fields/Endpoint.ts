import { Metric } from "./Metric";

export interface Endpoint {
    fields: Map<String, Metric>;
}
