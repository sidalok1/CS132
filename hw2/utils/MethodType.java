package utils;

import java.util.HashMap;

public record MethodType( HashMap<String, Types> parameters, Types returnType ) {}
