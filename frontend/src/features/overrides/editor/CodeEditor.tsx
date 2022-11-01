import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/theme-github";
import "ace-builds/src-noconflict/ext-language_tools"

import { useAppDispatch, useAppSelector } from '../../../app/hooks'

import { setTransformationJs} from '../overrideSlice';
import {useFetchOverrideQuery} from '../transformationsApiSlice';

export default function CodeEditor() {
  const dispatch = useAppDispatch();
  const selectedEdge = useAppSelector((state) => state.overrides.selectedEdge)|| 'none';
  const currentTxJs = useAppSelector((state) => state.overrides.currentTransformationJs)
  const {data , error, endpointName} = useFetchOverrideQuery(selectedEdge);
  const remoteTx = !!data ? data :  {injectionPoint:'none', transformationJs:'rt => rt'};
  const txJs = !!currentTxJs ? currentTxJs : remoteTx.transformationJs;
  return <AceEditor
  placeholder={
  `Provide a Javascript function here to tranform a request. e.g:
  (request) => {
    request.uri = '/api/v2/' + request.uri;
    request.headers['api-v2-token'] = 'test-token';
    return request;
  }
  `
  }
  mode="javascript"
  theme="github"
  name="blah2"
  onLoad={()=>{}}
  onChange={(value) => {dispatch(setTransformationJs(value))}}
  fontSize={14}
  showPrintMargin={true}
  showGutter={true}
  highlightActiveLine={true}
  value={txJs}
  setOptions={{
  enableBasicAutocompletion: true,
  enableLiveAutocompletion: true,
  enableSnippets: false,
  showLineNumbers: true,
  tabSize: 2,
  }}/>;
}