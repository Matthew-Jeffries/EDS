import {Resource} from "./Resource";
import {FieldTest} from "./FieldTest";
import {IsAny} from "./IsAny";

export class Test {
	resource: Resource;
	resourceUuid: string;
	isAny: IsAny;
	fieldTest: FieldTest[];
}
