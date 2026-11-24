import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/** 渲染期异常兜底，避免整页白屏。 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="form-page">
          <h1>页面出错了</h1>
          <p className="error">{this.state.error.message}</p>
          <button className="primary" onClick={() => this.setState({ error: null })}>
            重试
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
