package android.os;

/** This is a shadow class for AsyncTask which forces it to run synchronously. */
public abstract class AsyncTask<Params, Progress, Result> {

  protected abstract Result doInBackground(Params... params);

  protected void onPostExecute(Result result) {}

  protected void onProgressUpdate(Progress... values) {}

  public AsyncTask<Params, Progress, Result> execute(Params... params) {
    Result result = doInBackground(params);
    onPostExecute(result);
    return this;
  }
}
