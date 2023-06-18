export interface DifferencesQueryArgs {
    selectedEndpoint: string;
    selectedFieldPrefix: string;
    excludeNoise: boolean;
    includeWeights: boolean;
    start: number;
    end: number;
}
