import SaveIcon from '@mui/icons-material/Save';
import SettingsBackupRestoreIcon from '@mui/icons-material/SettingsBackupRestore';
import { Alert, Grid, IconButton, Snackbar, Tooltip, Typography } from '@mui/material';

import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/theme-github";
import "ace-builds/src-noconflict/ext-language_tools"

import { useAppDispatch, useAppSelector } from '../../../app/hooks'
import { openAlert, closeAlert} from '../overrideSlice';
import { setTransformationJs} from '../overrideSlice';
import {useFetchOverrideQuery, useUpdateOverrideMutation} from '../transformationsApiSlice';

export default function CodeEditor() {
  const dispatch = useAppDispatch();
  const selectedEdge = useAppSelector((state) => state.overrides.selectedEdge) || 'none';
  const alertIsOpen = useAppSelector((state) => state.overrides.alertIsOpen);
  const close = () => dispatch(closeAlert());
  const [updateOverride] = useUpdateOverrideMutation();
  const currentTxJs = useAppSelector((state) => state.overrides.currentTransformationJs);
  const {data} = useFetchOverrideQuery(selectedEdge);
  const remoteTx = !!data ? data :  {injectionPoint:'none', transformationJs:'rt => rt'};
  const txJs = !!currentTxJs ? currentTxJs : remoteTx.transformationJs;
  return <Grid>
          <Typography>Overriding traffic to <strong>{selectedEdge}</strong> with the following function:</Typography>
    <Tooltip title="Save transformation">
      <IconButton
        color="inherit"
        aria-label="save"
        onClick={
          ()=>selectedEdge && updateOverride({injectionPoint: selectedEdge, transformationJs: currentTxJs})
        .then(() => {
          dispatch(setTransformationJs(undefined));
          dispatch(openAlert(`Save success. Transformation will be applied to ${selectedEdge}.`));
        })
        }>
        <SaveIcon color="inherit"/>
      </IconButton>
    </Tooltip>

    <Tooltip title="Reset field mutation">
      <IconButton
        color="inherit"
        aria-label="reset"
        onClick={
          ()=>selectedEdge && updateOverride({injectionPoint: selectedEdge, transformationJs: '(request)=>(request)'})
          .then(() => {
            dispatch(setTransformationJs(undefined));
            dispatch(openAlert(`Reset success. No transformation will be applied to ${selectedEdge}.`));
          })
        }>
        <SettingsBackupRestoreIcon color="inherit"/>
      </IconButton>
    </Tooltip>
    <Snackbar open={!!alertIsOpen} autoHideDuration={3000} onClose={close}>
      <Alert onClose={close} severity="success" sx={{ width: '100%' }}>
        {alertIsOpen}
      </Alert>
    </Snackbar>
    <AceEditor
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
      name="ace-editor"
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
    }}/>
  </Grid>;
}