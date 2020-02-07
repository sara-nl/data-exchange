import "bootstrap";
import * as sapper from '@sapper/app';
import Controller from "./api/controller";

Controller.setup(sapper);

sapper.start({
    target: document.querySelector('#sapper')
});
