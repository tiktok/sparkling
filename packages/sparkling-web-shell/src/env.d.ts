declare global {
  interface LynxViewElement extends HTMLElement {
    /** Main-thread callback for NativeModules RPC calls from the Worker */
    onNativeModulesCall?: (
      name: string,
      data: unknown,
      moduleName: string,
    ) => Promise<unknown> | unknown;
  }

  interface HTMLElementTagNameMap {
    'lynx-view': LynxViewElement;
  }
}

export {};
