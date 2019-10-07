import "bootstrap";
import * as sapper from '@sapper/app';
import { token, email } from "./stores.ts";
import Controller from "./api/controller.ts";

token.useLocalStorage();
email.useLocalStorage();
Controller.setup(sapper);

sapper.start({
    target: document.querySelector('#sapper')
});
