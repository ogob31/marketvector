terraform {
  backend "s3" {
    bucket         = "magmarketvector"
    key            = "pro1"   
    region         = "us-east-1"
    dynamodb_table = "magmarketvector" 
  }
}
